package codingdojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
@Data
public class CustomerMatches {
    private Collection<Customer> duplicates = new ArrayList<>();
    private String matchTerm;
    private Customer customer;



    public boolean hasDuplicates() {
        return !duplicates.isEmpty();
    }

    public void addDuplicate(Customer duplicate) {
        duplicates.add(duplicate);
    }



}
