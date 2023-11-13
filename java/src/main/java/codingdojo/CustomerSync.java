package codingdojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerSync {

    private final CustomerDataLayer customerDataLayer;

    public CustomerSync(CustomerDataLayer customerDataLayer) {
        this.customerDataLayer = customerDataLayer;
    }


    public boolean syncWithDataLayer(ExternalCustomer externalCustomer) {

        Optional<CustomerSearchResult> searchResult;
        if (externalCustomer.isCompany()) {
            searchResult = loadCompany(externalCustomer);
        } else {
            searchResult = loadPerson(externalCustomer);
        }

        if (searchResult.isEmpty()) {
            return createNewCustomer(externalCustomer);
        }

        CustomerSearchResult customerSearchResult = searchResult.get();
        Customer customer = customerSearchResult.getCustomer();

        if (customer == null) {
            customer = new Customer();
            customer.setExternalId(externalCustomer.getExternalId());
            customer.setMasterExternalId(externalCustomer.getExternalId());
        }

        populateFields(externalCustomer, customer);

        boolean created = false;
        if (customer.getInternalId() == null) {
            customer = createCustomer(customer);
            created = true;
        } else {
            updateCustomer(customer);
        }
        updateContactInfo(externalCustomer, customer);

        if (customerSearchResult.hasDuplicates()) {
            for (Customer duplicate : customerSearchResult.getDuplicates()) {
                updateDuplicate(externalCustomer, duplicate);
            }
        }

        updateRelations(externalCustomer, customer);
        updatePreferredStore(externalCustomer, customer);

        return created;

    }

    private boolean createNewCustomer(ExternalCustomer externalCustomer) {
        Customer customer = new Customer();
        customer.setExternalId(externalCustomer.getExternalId());
        customer.setMasterExternalId(externalCustomer.getExternalId());
        populateFields(externalCustomer, customer);
        updateContactInfo(externalCustomer, customer);
        updateRelations(externalCustomer, customer);
        updatePreferredStore(externalCustomer, customer);
        createCustomer(customer);
        return true;
    }

    private void updateRelations(ExternalCustomer externalCustomer, Customer customer) {
        List<ShoppingList> consumerShoppingLists = externalCustomer.getShoppingLists();
        for (ShoppingList consumerShoppingList : consumerShoppingLists) {
            customer.addShoppingList(consumerShoppingList);
            customerDataLayer.updateShoppingList(consumerShoppingList);
            customerDataLayer.updateCustomerRecord(customer);
        }
    }

    private Customer updateCustomer(Customer customer) {
        return customerDataLayer.updateCustomerRecord(customer);
    }

    private void updateDuplicate(ExternalCustomer externalCustomer, Customer duplicate) {
        duplicate.setName(externalCustomer.getName());

        if (duplicate.getInternalId() == null) {
            createCustomer(duplicate);
        } else {
            updateCustomer(duplicate);
        }
    }

    private void updatePreferredStore(ExternalCustomer externalCustomer, Customer customer) {
        customer.setPreferredStore(externalCustomer.getPreferredStore());
    }

    private Customer createCustomer(Customer customer) {
        return customerDataLayer.createCustomerRecord(customer);
    }

    private void populateFields(ExternalCustomer externalCustomer, Customer customer) {
        customer.setName(externalCustomer.getName());
        if (externalCustomer.isCompany()) {
            customer.setCompanyNumber(externalCustomer.getCompanyNumber());
            customer.setCustomerType(CustomerType.COMPANY);
        } else {
            customer.setCustomerType(CustomerType.PERSON);
        }
    }

    private void updateContactInfo(ExternalCustomer externalCustomer, Customer customer) {
        customer.setAddress(externalCustomer.getPostalAddress());
    }

    public Optional<CustomerSearchResult> loadCompany(ExternalCustomer externalCustomer) {

        final String externalId = externalCustomer.getExternalId();
        final String companyNumber = externalCustomer.getCompanyNumber();

        return loadCompanyByExternalId(externalId, companyNumber)
                .or(() -> loadCompanyByCompanyNumber(companyNumber, externalId));
    }

    private Optional<CustomerSearchResult> loadCompanyByCompanyNumber(String companyNumber, String externalId) {
        Customer matchByCompanyNumber = customerDataLayer.findByCompanyNumber(companyNumber);
        if (matchByCompanyNumber == null)
            return Optional.empty();


        if (!CustomerType.COMPANY.equals(matchByCompanyNumber.getCustomerType())) {
            throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
        }
        String customerExternalId = matchByCompanyNumber.getExternalId();
        if (customerExternalId != null && !externalId.equals(customerExternalId)) {
            throw new ConflictException("Existing customer for externalCustomer " + companyNumber + " doesn't match external id " + externalId + " instead found " + customerExternalId);
        }
        matchByCompanyNumber.setExternalId(externalId);
        matchByCompanyNumber.setMasterExternalId(externalId);

        Customer duplicate = new Customer();
        duplicate.setExternalId(externalId);
        duplicate.setMasterExternalId(externalId);

        CustomerSearchResult customerSearchResult = CustomerSearchResult.found(List.of(duplicate), matchByCompanyNumber);
        return Optional.of(customerSearchResult);
    }

    private Optional<CustomerSearchResult> loadCompanyByExternalId(String externalId, String companyNumber) {
        Customer customerByExternalId = customerDataLayer.findByExternalId(externalId);
        if (customerByExternalId == null)
            return Optional.empty();


        if (!CustomerType.COMPANY.equals(customerByExternalId.getCustomerType())) {
            throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
        }
        String customerCompanyNumber = customerByExternalId.getCompanyNumber();
        CustomerSearchResult customerSearchResult;
        boolean companyNumberMatches = companyNumber.equals(customerCompanyNumber);
        Customer duplicateByMasterExternalId = customerDataLayer.findByMasterExternalId(externalId);
        List<Customer> duplicates = new ArrayList<>();
        if (duplicateByMasterExternalId != null)
            duplicates.add(duplicateByMasterExternalId);
        if (companyNumberMatches) {
            customerSearchResult = CustomerSearchResult.found(duplicates, customerByExternalId);
        } else {
            customerByExternalId.setMasterExternalId(null);
            duplicates.add(customerByExternalId);
            customerSearchResult = new CustomerSearchResult(duplicates, null);
        }

        return Optional.of(customerSearchResult);

    }

    public Optional<CustomerSearchResult> loadPerson(ExternalCustomer externalCustomer) {
        final String externalId = externalCustomer.getExternalId();
        Customer byExternalId = customerDataLayer.findByExternalId(externalId);

        if (byExternalId == null)
            return Optional.empty();

        CustomerSearchResult customerSearchResult = CustomerSearchResult.found(List.of(), byExternalId);

        if (!CustomerType.PERSON.equals(customerSearchResult.getCustomer().getCustomerType())) {
            throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a person");
        }

        return Optional.of(customerSearchResult);
    }
}
