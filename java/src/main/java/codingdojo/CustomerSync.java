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
        CustomerLoadResult loadResult = loadCustomer(externalCustomer);
        Customer customer = loadResult.getCustomer();

        populateFields(externalCustomer, customer);
        updateContactInfo(externalCustomer, customer);
        updateRelations(externalCustomer, customer);
        updatePreferredStore(externalCustomer, customer);


        for (Customer duplicate : loadResult.getDuplicates()) {
            duplicate.setName(externalCustomer.getName());
            customerDataLayer.save(duplicate);
        }

        return customerDataLayer.save(customer);
    }

    private CustomerLoadResult loadCustomer(ExternalCustomer externalCustomer) {
        Optional<CustomerLoadResult> searchResult;
        if (externalCustomer.isCompany()) {
            searchResult = loadCompany(externalCustomer);
        } else {
            searchResult = loadPerson(externalCustomer);
        }
        return searchResult.orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setExternalId(externalCustomer.getExternalId());
            newCustomer.setMasterExternalId(externalCustomer.getExternalId());

            return CustomerLoadResult.of(List.of(), newCustomer);
        });
    }

    private void updateRelations(ExternalCustomer externalCustomer, Customer customer) {
        List<ShoppingList> consumerShoppingLists = externalCustomer.getShoppingLists();
        for (ShoppingList consumerShoppingList : consumerShoppingLists) {
            customer.addShoppingList(consumerShoppingList);
            customerDataLayer.updateShoppingList(consumerShoppingList);
            customerDataLayer.updateCustomerRecord(customer);
        }
    }

    private void updatePreferredStore(ExternalCustomer externalCustomer, Customer customer) {
        customer.setPreferredStore(externalCustomer.getPreferredStore());
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

    public Optional<CustomerLoadResult> loadCompany(ExternalCustomer externalCustomer) {

        final String externalId = externalCustomer.getExternalId();
        final String companyNumber = externalCustomer.getCompanyNumber();

        return loadCompanyByExternalId(externalId, companyNumber)
                .or(() -> loadCompanyByCompanyNumber(companyNumber, externalId));
    }

    private Optional<CustomerLoadResult> loadCompanyByCompanyNumber(String companyNumber, String externalId) {
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

        CustomerLoadResult customerLoadResult = CustomerLoadResult.of(List.of(duplicate), matchByCompanyNumber);
        return Optional.of(customerLoadResult);
    }

    private Optional<CustomerLoadResult> loadCompanyByExternalId(String externalId, String companyNumber) {
        Customer customerByExternalId = customerDataLayer.findByExternalId(externalId);
        if (customerByExternalId == null)
            return Optional.empty();


        if (!CustomerType.COMPANY.equals(customerByExternalId.getCustomerType())) {
            throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
        }
        String customerCompanyNumber = customerByExternalId.getCompanyNumber();

        Customer duplicateByMasterExternalId = customerDataLayer.findByMasterExternalId(externalId);
        List<Customer> duplicates = new ArrayList<>();
        if (duplicateByMasterExternalId != null) {
            duplicates.add(duplicateByMasterExternalId);
        }

        boolean companyNumberMatches = companyNumber.equals(customerCompanyNumber);
        if (companyNumberMatches) {
            return Optional.of(CustomerLoadResult.of(duplicates, customerByExternalId));
        }

        customerByExternalId.setMasterExternalId(null);
        duplicates.add(customerByExternalId);

        Customer newCustomer = new Customer();
        newCustomer.setExternalId(externalId);
        newCustomer.setMasterExternalId(externalId);
        return Optional.of(CustomerLoadResult.of(duplicates, newCustomer));
    }

    public Optional<CustomerLoadResult> loadPerson(ExternalCustomer externalCustomer) {
        final String externalId = externalCustomer.getExternalId();
        Customer byExternalId = customerDataLayer.findByExternalId(externalId);

        if (byExternalId == null)
            return Optional.empty();

        CustomerLoadResult customerLoadResult = CustomerLoadResult.of(List.of(), byExternalId);

        if (!CustomerType.PERSON.equals(byExternalId.getCustomerType())) {
            throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a person");
        }

        return Optional.of(customerLoadResult);
    }
}
