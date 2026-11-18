
    create table client (
        id bigint not null auto_increment,
        type_client varchar(20) not null,
        numero_siren_tva varchar(32),
        nom varchar(150),
        prenom varchar(150),
        email varchar(254) not null,
        adresse_postale TEXT not null,
        raison_sociale varchar(255),
        primary key (id),
        check ((type_client in ('ENTREPRISE','PARTICULIER')))
    ) engine=InnoDB;

    create table facture (
        date_emission date not null,
        date_paiement date,
        montant_ht decimal(12,2) not null,
        prestation_date date not null,
        prestation_duree_minutes integer not null,
        prestation_heure_debut time(0) not null,
        taux_tva decimal(5,2) not null,
        id bigint not null auto_increment,
        seance_id bigint not null,
        client_numero_siren_tva varchar(32),
        numero varchar(32) not null,
        prestation_type varchar(100) not null,
        photographe_nom varchar(150) not null,
        client_email varchar(254) not null,
        client_identite varchar(320) not null,
        client_adresse_postale TEXT not null,
        statut_paiement enum ('ANNULEE','EN_ATTENTE','PAYEE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table photographe (
        actif bit not null,
        id bigint not null auto_increment,
        nom varchar(150) not null,
        email varchar(254) not null,
        primary key (id)
    ) engine=InnoDB;

    create table photographe_specialite (
        photographe_id bigint not null,
        specialite_id bigint not null,
        primary key (photographe_id, specialite_id)
    ) engine=InnoDB;

    create table seance (
        date_seance date not null,
        duree_minutes integer not null,
        heure_debut time(0) not null,
        client_id bigint not null,
        id bigint not null auto_increment,
        photographe_id bigint not null,
        specialite_id bigint not null,
        lieu varchar(255) not null,
        statut enum ('ANNULEE','CONFIRMEE','PLANIFIEE','TERMINEE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table specialite (
        id bigint not null auto_increment,
        libelle varchar(100) not null,
        primary key (id)
    ) engine=InnoDB;

    create table utilisateur (
        id bigint not null auto_increment,
        email varchar(254) not null,
        mot_de_passe varchar(255) not null,
        role enum ('GESTIONNAIRE','SECRETAIRE') not null,
        primary key (id)
    ) engine=InnoDB;

    alter table facture 
       add constraint UK6a7yy5kmqco6av948juwhyssq unique (seance_id);

    alter table facture 
       add constraint UKohh2s2qfc42flpuj3tby1wdn4 unique (numero);

    alter table photographe 
       add constraint UKsltxqxbva5fgswyv6f1ayuuk4 unique (email);

    alter table specialite 
       add constraint UKb6sv4pasw4wqdt82a43x43vpj unique (libelle);

    alter table utilisateur 
       add constraint UKrma38wvnqfaf66vvmi57c71lo unique (email);

    alter table facture 
       add constraint FK2iiwh1g11p4jixxmj8vgc5qwn 
       foreign key (seance_id) 
       references seance (id);

    alter table photographe_specialite 
       add constraint FK4xscx0hca30qbdmk85pr6cu5n 
       foreign key (specialite_id) 
       references specialite (id);

    alter table photographe_specialite 
       add constraint FKcg4xhsdv4eob2m41nfye7xh0n 
       foreign key (photographe_id) 
       references photographe (id);

    alter table seance 
       add constraint FK4ik1u930079067v1k0tdwyuk5 
       foreign key (client_id) 
       references client (id);

    alter table seance 
       add constraint FKanab3kjnnhtn5c9iuhji4cvwc 
       foreign key (photographe_id) 
       references photographe (id);

    alter table seance 
       add constraint FKjrr7lxxntsy0t1ivysdu8o9i3 
       foreign key (specialite_id) 
       references specialite (id);
