package codingdojo;

import lombok.Value;

@Value
public class Address {
    private final String street;
    private final String city;
    private final String postalCode;
}
