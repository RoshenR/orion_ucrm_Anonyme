package com.openclassroom.devops.orion.microcrm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
public class Organization {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  public long getId() {
    return id;
  }

  @ManyToMany(cascade = CascadeType.ALL)
  private List<Person> persons;

  public List<Person> addPerson(Person person) {
    if (this.persons == null) {
      this.persons = new ArrayList<Person>();
    }
    this.persons.add(person);
    return this.persons;
  }

  public List<Person> removePerson(Person person) {
    if (this.persons == null) {
      this.persons = new ArrayList<Person>();
    }
    this.persons.remove(person);
    return this.persons;
  }

  public List<Person> getPersons() {
    return persons;
  }

  public void setPersons(List<Person> persons) {
    this.persons = persons;
  }

  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  private Date createdAt;

  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  private Date updatedAt;

  public Date getCreatedAt() {
    return createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }


}
