package com.example.Weather.Assistant.model;

import com.example.Weather.Assistant.repository.UserState;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;


@Entity
@Table(name = "usersData")
public class User {

    private UserState userState = UserState.Default;
    private int timeZone;
    @Id
    private Long chatID;

    private String firstNameUser;

    private String city;
    private  Boolean subscription;
    private String timeSubscription;

    public int getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(int timeZone) {
        this.timeZone = timeZone;
    }

    public UserState getUserState(){return userState;}
    public void setUserState(UserState userState){this.userState = userState;}
    public String getTimeSubscription(){return timeSubscription;}
    public void setTimeSubscription(String timeSubscription){this.timeSubscription = timeSubscription;}
    public Boolean getSubscription(){return subscription;}
    public void setSubscription(Boolean subscription){this.subscription = subscription;}
    public Long getChatID() {
        return chatID;
    }

    public void setChatID(Long chatID) {
        this.chatID = chatID;
    }

    public String getNameUser() {
        return firstNameUser;
    }

    public void setNameUser(String nameUser) {
        this.firstNameUser = nameUser;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "User{" +
                "chatID=" + chatID +
                ", firstNameUser='" + firstNameUser + '\'' +
                ", subscription='" + subscription + '\'' +
                ", timeSubscription='" + timeSubscription + '\'' +
                '}';
    }
}
