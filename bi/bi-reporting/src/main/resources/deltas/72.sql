drop table dau_mau;

create table dau_mau(
	date date not null distkey sortkey,
	interval varchar(1) not null,
	platform varchar(16) not null,
	game_type varchar(32) not null,
	num_players int not null
);


GRANT SELECT ON dau_mau TO GROUP READ_ONLY;
GRANT ALL ON dau_mau TO GROUP READ_WRITE;
GRANT ALL ON dau_mau TO GROUP SCHEMA_MANAGER;