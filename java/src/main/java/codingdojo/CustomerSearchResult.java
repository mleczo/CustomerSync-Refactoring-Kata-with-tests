package codingdojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class CustomerSearchResult {
    private Collection<Customer> duplicates = new ArrayList<>();
    private MatchTerm matchTerm;
    private Customer customer;

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
