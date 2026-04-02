package com.openclassroom.devops.orion.microcrm;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class PersonRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PersonRepository personRepository;

    @Test
    public void whenFindByEmail_thenReturnPerson() {
        // given
        Person jdoe = new Person();
        jdoe.setEmail("jdoe@example.net");
        entityManager.persist(jdoe);
        entityManager.flush();

        // when
        Optional<Person> found = personRepository.findByEmail("jdoe@example.net");

        assertEquals(jdoe.getEmail(), found.get().getEmail());
    }
}