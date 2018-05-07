create sequence hibernate_sequence start 1 increment 1
create table compute_order (disk int4, image_name varchar(255), memory int4, public_key varchar(255), vcpu int4, id varchar(255) not null, primary key (id))
create table tb_order (id varchar(255) not null, fulfilled_time int8, order_state varchar(255) not null, providing_member varchar(255), requesting_member varchar(255), fed_token_id int8, local_token_id int8, order_instance varchar(255), primary key (id))
create table tb_order_instance (id varchar(255) not null, role varchar(255) not null, primary key (id))
create table tb_token (id int8 not null, access_id varchar(255) not null, user_id varchar(255), primary key (id))
create table token$user (id varchar(255) not null, name varchar(255) not null, primary key (id))
alter table tb_token add constraint UK_ee05r89vxcmpl6154ta2jp505 unique (access_id)
alter table token$user add constraint UK_7tx70uc0wfyp12u0re106aa9p unique (name)
alter table compute_order add constraint FK7jm1bglfnvwohks4upxhqtnj foreign key (id) references tb_order
alter table tb_order add constraint FKron2aw5yiv8cucfog6u9d3vqk foreign key (fed_token_id) references tb_token
alter table tb_order add constraint FK49xbbncqb2m7k5hn3c18l05l0 foreign key (local_token_id) references tb_token
alter table tb_order add constraint FKmc4qw6wq5bnqvh66b6wttpdvr foreign key (order_instance) references tb_order_instance
alter table tb_token add constraint FKn9yjg47xvtqkwp6tpk8he7bfj foreign key (user_id) references token$user
