drop table if exists BRM;
drop table if exists BRM_head;

create table BRM (id bigint,tm timestamp,var text, primary key(id));
create table BRM_head (id int,max bigint,pos bigint,last bigint);
insert into BRM_head (id,max,pos,last) values(1,2000000,0,0);

drop table if exists FP7;
drop table if exists FP7_head;
create table FP7 (id bigint,tm timestamp,var text, primary key(id));
create table FP7_head (id int,max bigint,pos bigint,last bigint);
insert into FP7_head (id,max,pos,last) values(1,2000000,0,0);
