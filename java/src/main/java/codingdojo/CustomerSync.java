package codingdojo;

import java.util.List;

public class CustomerSync {

    private final CustomerDataLayer customerDataLayer;

    public CustomerSync(CustomerDataLayer customerDataLayer) {
        this.customerDataLayer = customerDataLayer;
    }


    public boolean syncWithDataLayer(ExternalCustomer externalCustomer) {

        CustomerSearchResult customerSearchResult;
        if (externalCustomer.isCompany()) {
            customerSearchResult = loadCompany(externalCustomer);
        } else {
            customerSearchResult = loadPerson(externalCustomer);
        }
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

    public CustomerSearchResult loadCompany(ExternalCustomer externalCustomer) {

        final String externalId = externalCustomer.getExternalId();
        final String companyNumber = externalCustomer.getCompanyNumber();

        CustomerSearchResult customerSearchResult = new CustomerSearchResult();
        Customer matchByExternalId = customerDataLayer.findByExternalId(externalId);
        if (matchByExternalId != null) {

            customerSearchResult.setCustomer(matchByExternalId);
            customerSearchResult.setMatchTerm(CustomerSearchResult.MatchTerm.EXTERNAL_ID);
            Customer matchByMasterId = customerDataLayer.findByMasterExternalId(externalId);
            if (matchByMasterId != null)
                customerSearchResult.addDuplicate(matchByMasterId);
            if (customerSearchResult.getCustomer() != null && !CustomerType.COMPANY.equals(customerSearchResult.getCustomer().getCustomerType())) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
            }
            String customerCompanyNumber = customerSearchResult.getCustomer().getCompanyNumber();
            if (!companyNumber.equals(customerCompanyNumber)) {
                customerSearchResult.getCustomer().setMasterExternalId(null);
                customerSearchResult.addDuplicate(customerSearchResult.getCustomer());
                customerSearchResult.setCustomer(null);
                customerSearchResult.setMatchTerm(null);
            }
        } else {
            Customer matchByCompanyNumber = customerDataLayer.findByCompanyNumber(companyNumber);
            if (matchByCompanyNumber == null)
                return new CustomerSearchResult();
            customerSearchResult.setCustomer(matchByCompanyNumber);
            customerSearchResult.setMatchTerm(CustomerSearchResult.MatchTerm.COMPANY_NUMBER);

            if (customerSearchResult.getCustomer() != null && !CustomerType.COMPANY.equals(customerSearchResult.getCustomer().getCustomerType())) {
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
        }


        if (customerSearchResult.getMatchTerm() == CustomerSearchResult.MatchTerm.EXTERNAL_ID) {

        } else if (customerSearchResult.getMatchTerm() == CustomerSearchResult.MatchTerm.COMPANY_NUMBER) {

        }

        return customerSearchResult;
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
