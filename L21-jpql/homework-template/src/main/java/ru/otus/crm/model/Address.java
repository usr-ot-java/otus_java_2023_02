package ru.otus.crm.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "address")
public class Address implements Cloneable {

    @Id
    @SequenceGenerator(name = "address_gen", sequenceName = "address_id_seq",
            initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "address_gen", strategy = GenerationType.SEQUENCE)
    private Long id;

    private String street;

    @OneToOne(mappedBy = "address", cascade = CascadeType.PERSIST)
    private Client client;

    public Address(String street) {
        this.street = street;
    }

    public Address(String street, Client client) {
        this.street = street;
        this.client = client;
    }

    public Address(Long id, String street) {
        this.id = id;
        this.street = street;
    }

    public Address(Long id, String street, Client client) {
        this.id = id;
        this.street = street;
        this.client = client;
    }

    @Override
    public Address clone() {
        return new Address(id, street, client);
    }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", street ='" + street + '\'' +
                ", client='" + (client != null ? client.getId() : "null") + '\'' +
                '}';
    }
}
