create table bank_accounts (
number varchar(64) primary key,
owner varchar(128) not null,
balance numeric(19,2) not null,
currency varchar(3) not null
);