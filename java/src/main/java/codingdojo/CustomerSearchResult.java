package codingdojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class CustomerSearchResult {
    private Collection<Customer> duplicates = new ArrayList<>();
    private MatchTerm matchTerm;
    private Customer customer;

    public static CustomerSearchResult found(Customer customer, MatchTerm matchTerm) {
        CustomerSearchResult customerSearchResult = new CustomerSearchResult();
        customerSearchResult.setCustomer(customer); //todo make this class immutable
        customerSearchResult.setMatchTerm(matchTerm);
        return customerSearchResult;
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
