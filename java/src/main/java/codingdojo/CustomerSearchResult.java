package codingdojo;

import lombok.Data;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collection;

@Value
public class CustomerSearchResult {
    Collection<Customer> duplicates = new ArrayList<>();
    MatchTerm matchTerm;
    Customer customer;

    public static CustomerSearchResult found(Customer customer, MatchTerm matchTerm) {
        return new CustomerSearchResult(matchTerm, customer);
    }

    enum MatchTerm {
        EXTERNAL_ID, COMPANY_NUMBER
    }


    public boolean hasDuplicates() {
        return !duplicates.isEmpty();
    }

    public void addDuplicate(Customer duplicate) {
        duplicates.add(duplicate);
    }


}
