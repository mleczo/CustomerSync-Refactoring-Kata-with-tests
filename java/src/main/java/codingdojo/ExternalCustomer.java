package codingdojo;

import lombok.Data;

import java.util.List;
@Data
public class ExternalCustomer {
    private Address address;
    private String name;
    private String preferredStore;
    private List<ShoppingList> shoppingLists;
    private String externalId;
    private String companyNumber;


    public boolean isCompany() {
        return companyNumber != null;
    }
    public Address getPostalAddress() {
        return address;
    }
}
