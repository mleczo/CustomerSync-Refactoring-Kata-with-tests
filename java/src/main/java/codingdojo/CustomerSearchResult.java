package codingdojo;

import lombok.Data;
import lombok.Value;

import java.util.*;

@Value
public class CustomerSearchResult {
    List<Customer> duplicates;
    Customer customer;

    public static CustomerSearchResult found(List<Customer> duplicates, Customer customer) {
        List<Customer> dupsImmutable = Collections.unmodifiableList(new ArrayList<>(duplicates));
        return new CustomerSearchResult(dupsImmutable, customer);
    }

}
