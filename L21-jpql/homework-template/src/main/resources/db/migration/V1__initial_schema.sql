-- Для @GeneratedValue(strategy = GenerationType.IDENTITY)
/*
create table client
(
    id   bigserial not null primary key,
    name varchar(50)
);

 */

create sequence address_id_seq start with 1 increment by 1;

create table address
(
    id bigint not null primary key,
    street varchar(100)
);

-- Для @GeneratedValue(strategy = GenerationType.SEQUENCE)
create sequence client_SEQ start with 1 increment by 1;

create table client
(
    id   bigint not null primary key,
    name varchar(50),
    address_id bigint,
    foreign key(address_id) references address(id) on delete cascade
);

create sequence phone_id_seq start with 1 increment by 1;

create table phone
(
    id bigint not null primary key,
    number varchar(11),
    client_id bigint,
    foreign key(client_id) references client(id) on delete set null
);


