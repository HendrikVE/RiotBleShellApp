package de.vanappsteer.riotbleshell.inputvalidator;

public interface InputValidator<T> {

    boolean validate(T object);

    String getValidRangeString();

}
