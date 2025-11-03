create table group_members
(
    id        bigint auto_increment
        primary key,
    group_id  bigint      not null,
    username  varchar(20) not null,
    join_time datetime    not null,
    role      char(8)     not null
);

create table group_messages
(
    id          bigint auto_increment
        primary key,
    sender_name varchar(20) not null,
    group_id    int         not null,
    content     longtext    not null,
    time        datetime    not null
);

create table `groups`
(
    group_id          int auto_increment
        primary key,
    group_name        varchar(50)   not null,
    group_avatar_path longtext      not null,
    creator_username  varchar(20)   not null,
    member_count      int default 0 null,
    create_time       datetime      not null
);

create table message
(
    id       bigint auto_increment
        primary key,
    sender   varchar(20)  null,
    receiver varchar(20)  null,
    content  varchar(255) null,
    sendtime datetime     null on update CURRENT_TIMESTAMP
)
    charset = utf8mb3;

create table user
(
    id          int auto_increment
        primary key,
    username    varchar(20) null,
    password    varchar(20) null,
    avatar_path longtext    null
)
    charset = utf8mb3;

create index username
    on user (username);

create table userrelation
(
    id         int auto_increment
        primary key,
    masteruser varchar(20) null,
    slaveuser  varchar(20) null,
    relation   int         not null
)
    charset = utf8mb4;

