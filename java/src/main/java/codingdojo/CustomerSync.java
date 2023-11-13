package codingdojo;

import java.util.List;
import java.util.Optional;

public class CustomerSync {

    private final CustomerDataLayer customerDataLayer;

    public CustomerSync(CustomerDataLayer customerDataLayer) {
        this.customerDataLayer = customerDataLayer;
    }


    public boolean syncWithDataLayer(ExternalCustomer externalCustomer) {

        Optional<CustomerSearchResult>  searchResult;
        if (externalCustomer.isCompany()) {
            searchResult = loadCompany(externalCustomer);
        } else {
            searchResult = Optional.of(loadPerson(externalCustomer));
        }
        CustomerSearchResult customerSearchResult = searchResult.orElse(new CustomerSearchResult());
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
        if (duplicate == null) {
            duplicate = new Customer();
            duplicate.setExternalId(externalCustomer.getExternalId());
            duplicate.setMasterExternalId(externalCustomer.getExternalId());
        }

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

        Customer customerByExternalId = customerDataLayer.findByExternalId(externalId);
        if (customerByExternalId != null) {
            CustomerSearchResult customerSearchResult = CustomerSearchResult.found(customerByExternalId, CustomerSearchResult.MatchTerm.EXTERNAL_ID);

            if (!CustomerType.COMPANY.equals(customerSearchResult.getCustomer().getCustomerType())) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
            }
            String customerCompanyNumber = customerByExternalId.getCompanyNumber();
            if (!companyNumber.equals(customerCompanyNumber)) {
                customerByExternalId.setMasterExternalId(null);
                customerSearchResult.addDuplicate(customerSearchResult.getCustomer());
                customerSearchResult.setCustomer(null);
                customerSearchResult.setMatchTerm(null);
            }
            Customer duplicateByMasterExternalId = customerDataLayer.findByMasterExternalId(externalId);
            if (duplicateByMasterExternalId != null)
                customerSearchResult.addDuplicate(duplicateByMasterExternalId);
            return Optional.of(customerSearchResult);
        } else {
            Customer matchByCompanyNumber = customerDataLayer.findByCompanyNumber(companyNumber);
            if (matchByCompanyNumber == null)
                return Optional.empty();

            CustomerSearchResult customerSearchResult = CustomerSearchResult.found(matchByCompanyNumber, CustomerSearchResult.MatchTerm.COMPANY_NUMBER);

            if (!CustomerType.COMPANY.equals(customerSearchResult.getCustomer().getCustomerType())) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
            }
            String customerExternalId = customerSearchResult.getCustomer().getExternalId();
            if (customerExternalId != null && !externalId.equals(customerExternalId)) {
                throw new ConflictException("Existing customer for externalCustomer " + companyNumber + " doesn't match external id " + externalId + " instead found " + customerExternalId);
            }
            Customer customer = customerSearchResult.getCustomer();
            customer.setExternalId(externalId);
            customer.setMasterExternalId(externalId);
            customerSearchResult.addDuplicate(null);
            return Optional.of(customerSearchResult);
        }


    }

    public CustomerSearchResult loadPerson(ExternalCustomer externalCustomer) {
        final String externalId = externalCustomer.getExternalId();

        CustomerSearchResult matches = new CustomerSearchResult();
        Customer matchByPersonalNumber = customerDataLayer.findByExternalId(externalId);
        matches.setCustomer(matchByPersonalNumber);
        if (matchByPersonalNumber != null)
            matches.setMatchTerm(CustomerSearchResult.MatchTerm.EXTERNAL_ID);
        CustomerSearchResult customerSearchResult = matches;

        if (customerSearchResult.getCustomer() != null) {
            if (!CustomerType.PERSON.equals(customerSearchResult.getCustomer().getCustomerType())) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a person");
            }

            if (CustomerSearchResult.MatchTerm.EXTERNAL_ID != customerSearchResult.getMatchTerm()) {
                Customer customer = customerSearchResult.getCustomer();
                customer.setExternalId(externalId);
                customer.setMasterExternalId(externalId);
            }
        }

        return customerSearchResult;
    }
}
