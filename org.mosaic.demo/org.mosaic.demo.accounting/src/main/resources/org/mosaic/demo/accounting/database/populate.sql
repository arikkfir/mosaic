INSERT INTO voluntarily.organizations ( id, name, facebook_page, geo_lat, geo_long )
VALUES
( 1, 'The Red Cross', 'http://www.facebook.com/redcross', '12345678', '12345678' ),
( 2, 'Crazy Johny', NULL, '12345678', '12345678' ),
( 3, 'Kabala Institute', 'http://www.facebook.com/kabalala', '12345678', '12345678' ),
( 4, 'Wintroub Kindergarden', 'http://www.facebook.com/wingarden', '12345678', '12345678' ),
( 5, 'Dididudadu', 'http://www.facebook.com/dididu', '12345678', '12345678' )

INSERT INTO voluntarily.campaigns ( id, organization_id, name, description, facebook_page, start_date, end_date )
VALUES
( 1, 1, 'Join the Red Cross!', 'We have Swiss knifes!', 'http://www.facebook.com/redcross-join-us', '2014-01-01 10:00:00', '2014-06-01 23:00:00' ),
( 2, 1, 'Donate to the Red Cross!', 'We need more knifes!', 'http://www.facebook.com/more-crosses', '2014-01-10 10:00:00', '2014-01-29 23:59:59' )

INSERT INTO voluntarily.tasks (id, campaign_id, name, description, participation_limit, views, start_date, end_date)
VALUES
    ( 1, 1, 'Send 1 package', 'Send a package to Joe!', 10, 0, '2014-01-01 10:00:00', '2014-06-01 23:00:00' ),
    ( 2, 1, 'Send 3 packages', 'Send a package to Joe!', 10, 0, '2014-01-01 10:00:00', '2014-06-01 23:00:00' ),
    ( 3, 1, 'Give a hug', 'Send a package to Joe!', 10, 0, '2014-01-01 10:00:00', '2014-06-01 23:00:00' ),
    ( 4, 2, 'Drive someone', 'to work', 10, 0, '2014-01-01 10:00:00', '2014-06-01 23:00:00' ),
    ( 5, 2, 'Drive someone else', 'to work', 10, 0, '2014-01-01 10:00:00', '2014-06-01 23:00:00' );

COMMIT;
