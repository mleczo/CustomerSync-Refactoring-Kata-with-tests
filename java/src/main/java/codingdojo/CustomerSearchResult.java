package codingdojo;

import lombok.Data;
import lombok.Value;

import java.util.*;

@Value
public class CustomerSearchResult {
    List<Customer> duplicates;
    MatchTerm matchTerm;
    Customer customer;

    public static CustomerSearchResult found(List<Customer> duplicates, Customer customer, MatchTerm matchTerm) {
        List<Customer> dupsImmutable = Collections.unmodifiableList(new ArrayList<>(duplicates));
        return new CustomerSearchResult(dupsImmutable, matchTerm, customer);
    }

    enum MatchTerm {
        EXTERNAL_ID, COMPANY_NUMBER
    }


    public boolean hasDuplicates() {
        return !duplicates.isEmpty();
    }


}
