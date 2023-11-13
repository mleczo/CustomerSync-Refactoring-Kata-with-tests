package codingdojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerLoadResult {
    List<Customer> duplicates;
    Customer customer;

    public static CustomerLoadResult of(List<Customer> duplicates, Customer customer) {
        List<Customer> dupsImmutable = Collections.unmodifiableList(new ArrayList<>(duplicates));
        return new CustomerLoadResult(dupsImmutable, customer);
    }

}
