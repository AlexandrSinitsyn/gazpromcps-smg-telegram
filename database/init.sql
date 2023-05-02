create table job
(
    id bigserial primary key ,
    stage varchar(255) not null ,
    gen_plan varchar(255) not null ,
    master varchar(255) not null ,
    title varchar(255) not null ,
    measurement varchar(255) not null ,
    is_active boolean not null default true ,
    timestamp timestamp not null default now()
);

create table completed
(
    id bigserial primary key ,
    job_id bigint not null ,
    constraint fk_job foreign key (job_id) references job ( id ) on delete cascade ,
    count float not null ,
    timestamp timestamp not null default now(),
        check ( count > 0 )
);

create table users
(
    id bigserial primary key ,
    name varchar(255) not null ,
    superuser boolean not null default false ,
    groups bigint[] ,
    admin_in bigint[] ,
    timestamp timestamp not null default now()
);
