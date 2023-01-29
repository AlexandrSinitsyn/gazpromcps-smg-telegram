create table job
(
    id serial primary key ,
    section_number float not null ,
    title varchar(255) not null ,
    measurement varchar(255) not null ,
    timestamp timestamp not null default now(),
        check ( section_number > 0 )
);

create table completed
(
    id serial primary key ,
    job_id int not null ,
    constraint fk_job foreign key (job_id) references job ( id ) on delete cascade ,
    count int not null ,
    timestamp timestamp not null default now(),
        check ( count > 0 )
);

create table users
(
    id serial primary key ,
    name varchar(255) not null ,
    superuser boolean not null default false ,
    groups int[] ,
    admin_in int[] ,
    timestamp timestamp not null default now()
);
