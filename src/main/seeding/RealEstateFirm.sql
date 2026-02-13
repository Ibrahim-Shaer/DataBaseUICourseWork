
CREATE OR REPLACE TYPE privileges_type AS OBJECT (
    can_read CHAR(1),
    can_write CHAR(1),
    can_delete CHAR(1)
);
/


CREATE TABLE Roles (
    role_id INT PRIMARY KEY,
    role_type VARCHAR2(50),
    role_privilages privileges_type
);

CREATE TABLE Person (
    person_id INT PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    phone_number VARCHAR2(20)
);

CREATE TABLE Clients (
    person_id INT PRIMARY KEY REFERENCES Person(person_id),
    budget NUMBER(15, 2),
    area_interested_in VARCHAR2(255)
);

CREATE TABLE Agents (
    person_id INT PRIMARY KEY REFERENCES Person(person_id),
    salary NUMBER(10, 2),
    hire_date DATE
);

CREATE TABLE Property_Owners (
    person_id INT PRIMARY KEY REFERENCES Person(person_id),
    property_id INT
);

CREATE TABLE Properties (
    property_id INT PRIMARY KEY,
    price NUMBER(15, 2),
    square_meters NUMBER(10, 2),
    location VARCHAR2(100)
);

CREATE TABLE Apartment (
    property_id INT PRIMARY KEY REFERENCES Properties(property_id),
    number_of_rooms INT,
    n_of_bathrooms INT,
    floor INT
);

CREATE TABLE House (
    property_id INT PRIMARY KEY REFERENCES Properties(property_id),
    garden_size_m2 NUMBER(10, 2),
    n_of_bathrooms INT,
    number_of_rooms INT,
    number_of_floors INT,
    has_garage CHAR(1)
);

CREATE TABLE Garage (
    property_id INT PRIMARY KEY REFERENCES Properties(property_id)
);

CREATE TABLE Preferences (
    preference_id INT PRIMARY KEY,
    preference_type VARCHAR2(100)
);

CREATE TABLE User_Roles (
    person_id INT REFERENCES Person(person_id),
    role_id INT REFERENCES Roles(role_id),
    PRIMARY KEY (person_id, role_id)
);

CREATE TABLE Client_Preferences (
    person_id INT REFERENCES Clients(person_id),
    preference_id INT REFERENCES Preferences(preference_id),
    PRIMARY KEY (person_id, preference_id)
);

CREATE TABLE Listings (
    listing_id INT PRIMARY KEY,
    property_id INT REFERENCES Properties(property_id),
    type_of_listing VARCHAR2(50),
    notes VARCHAR2(500),
    description VARCHAR2
);


CREATE TABLE Property_Images (
    image_id INT PRIMARY KEY,
    listing_id INT REFERENCES Listings(listing_id),
    caption VARCHAR2(200),
    upload_date DATE
);


CREATE TABLE Visits (
    visit_id INT PRIMARY KEY,
    client_id INT REFERENCES Clients(person_id),
    agent_id INT REFERENCES Agents(person_id),
    property_id INT REFERENCES Properties(property_id),
    date_time TIMESTAMP,
    notes VARCHAR2(500)
);


CREATE TABLE Successful_Deals (
    deal_id INT PRIMARY KEY,
    property_id INT REFERENCES Properties(property_id),
    final_price NUMBER(15, 2)
);



INSERT INTO Roles VALUES (1, 'Admin', privileges_type('Y', 'Y', 'Y'));
INSERT INTO Roles VALUES (2, 'Agent', privileges_type('Y', 'Y', 'N'));
INSERT INTO Roles VALUES (3, 'Client', privileges_type('Y', 'N', 'N'));


INSERT INTO Preferences VALUES (1, 'Luxury Apartments');
INSERT INTO Preferences VALUES (2, 'Houses with Garden');


INSERT INTO Person VALUES (101, 'Ivan', 'Ivanov', 'ivan@email.com', '0888111222');
INSERT INTO Person VALUES (102, 'Maria', 'Petrova', 'maria@email.com', '0888333444');
INSERT INTO Person VALUES (103, 'Georgi', 'Todorov', 'georgi@email.com', '0888555666');


INSERT INTO User_Roles VALUES (101, 1); -- Иван е Админ
INSERT INTO User_Roles VALUES (102, 2); -- Мария е Агент
INSERT INTO User_Roles VALUES (103, 3); -- Георги е Клиент


INSERT INTO Agents VALUES (102, 3500.00, TO_DATE('2023-01-15', 'YYYY-MM-DD'));
INSERT INTO Clients VALUES (103, 250000.00, 'Sofia Center');


INSERT INTO Properties VALUES (501, 120000, 75.5, 'Sofia');
INSERT INTO Properties VALUES (502, 350000, 250.0,  'Plovdiv');


INSERT INTO Apartment VALUES (501, 3, 1, 4); -- 3 стаи, 1 баня, 4-ти етаж
INSERT INTO House VALUES (502, 150.0, 3, 6, 2, 'Y'); -- Двор 150м2, 3 бани, 6 стаи, 2 етажа, гараж


INSERT INTO Listings VALUES (1001, 501, 'Sale', 'Sunny apartment in the heart of Sofia', 'No pets allowed');


INSERT INTO Successful_Deals VALUES (1, 501, 115000.00);


INSERT INTO Property_Owners (person_id, property_id) VALUES (101, 501);


INSERT INTO Client_Preferences (person_id, preference_id) VALUES (103, 1);


INSERT INTO Visits (visit_id, client_id, agent_id, property_id, date_time, notes)
VALUES (201, 103, 102, 501, CURRENT_TIMESTAMP, 'Хареса имота');


