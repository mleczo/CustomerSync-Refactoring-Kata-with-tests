package codingdojo;

public interface CustomerDataLayer {
    default boolean save(Customer customer) {
        if (customer.getInternalId() == null) {
            createCustomerRecord(customer);
            return true;
        } else {
            updateCustomerRecord(customer);
            return false;
        }
    }

    Customer updateCustomerRecord(Customer customer);

    Customer createCustomerRecord(Customer customer);

    void updateShoppingList(ShoppingList consumerShoppingList);

    Customer findByExternalId(String externalId);

    Customer findByMasterExternalId(String externalId);

    Customer findByCompanyNumber(String companyNumber);
}
