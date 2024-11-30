create table biz_account
(
    id    bigint auto_increment primary key,
    uname varchar(20) not null,
    pwd   varchar(20) not null,
    role  varchar(64) not null
);

create table biz_app_version
(
    id          bigint auto_increment primary key,
    name        varchar(128)                        not null,
    description varchar(128) null,
    path        text                                not null,
    md5         varchar(64)                         not null,
    platform    varchar(64)                         not null,
    create_time timestamp default CURRENT_TIMESTAMP not null
);

create table biz_group
(
    id            bigint auto_increment primary key,
    name          varchar(64) not null,
    description   varchar(128) null,
    default_group tinyint     not null,
    tenant_id     bigint      not null
);

create table biz_group_member
(
    id        bigint auto_increment primary key,
    group_id  bigint not null,
    member_id bigint not null,
    tenant_id bigint not null
);

create table biz_group_route
(
    id        bigint auto_increment primary key,
    group_id  bigint not null,
    route_id  bigint not null,
    tenant_id bigint not null
);

create table biz_group_rule
(
    id        bigint auto_increment primary key,
    group_id  bigint not null,
    rule_id   bigint not null,
    tenant_id bigint not null
);

create table biz_group_vnat
(
    id        bigint auto_increment primary key,
    group_id  bigint not null,
    vnat_id   bigint not null,
    tenant_id bigint not null
);

create table biz_node
(
    id          bigint auto_increment primary key,
    name        varchar(64) null,
    description varchar(128) null,
    mac         varchar(64) not null,
    vip         varchar(16) null,
    enable      tinyint     not null,
    tenant_id   bigint      not null
);

create table biz_route
(
    id          bigint auto_increment primary key,
    name        varchar(64) not null,
    description varchar(128) null,
    destination varchar(128) null,
    enable      tinyint     not null,
    tenant_id   bigint      not null
);

create table biz_route_node_item
(
    id        bigint auto_increment primary key,
    route_id  bigint not null,
    node_id   bigint not null,
    tenant_id bigint not null
);

create table biz_route_rule
(
    id          bigint auto_increment primary key,
    name        varchar(64) not null,
    description varchar(128) null,
    strategy    varchar(16) not null,
    direction   varchar(16) not null,
    level       int         not null,
    rule_list   text        not null,
    enable      tinyint     not null,
    tenant_id   bigint      not null
);

create table biz_tenant
(
    id          bigint auto_increment primary key,
    name        varchar(64)        not null,
    description varchar(128) null,
    code        varchar(64)        not null,
    cidr        varchar(64)        not null,
    config      text               not null,
    enable      tinyint            not null,
    ip_index    int     default -1 not null,
    account_id  bigint             not null,
    node_grant  tinyint default 0  not null
);

create table biz_vip_pool
(
    id        bigint auto_increment primary key,
    vip       varchar(16)       not null,
    used      tinyint default 0 not null,
    tenant_id bigint            not null
);

create table biz_vnat
(
    id          bigint auto_increment primary key,
    name        varchar(64) not null,
    description varchar(128) null,
    src_cidr    varchar(16) not null,
    dst_cidr    varchar(16) not null,
    enable      tinyint     not null,
    tenant_id   bigint      not null
);

create table biz_vnat_node_item
(
    id        bigint auto_increment primary key,
    vnat_id   bigint not null,
    node_id   bigint not null,
    tenant_id bigint not null
);

INSERT INTO biz_account (uname, pwd, role)
VALUES ('root', 'thunder', 'Root');