INSERT INTO USERS (USERNAME)
VALUES
    ('walter'),
    ('jesse'),

    ('walter'),
    ('hank'),
    ('jesse'),
    ('mike'),
    ('gus'),
    ('skyler'),

    ('walter'),
    ('hank'),
    ('mike'),
    ('gus'),
    ('skyler'); 

INSERT INTO EVENTS (CREATION_DATE, INVITE_CODE, LAST_ACTIVITY, TITLE)
VALUES
    ('20250811', 999999, '20260503', 'Cooking class'),
    ('20230813', 123456, '20240101', 'OWEE 2023'),
    ('20231001', 1111111, '20240102', 'Efteling');

INSERT INTO USER_EVENT (USER_ID, EVENT_ID)
VALUES
    (1, 1), (2, 1),
    (3, 2), (4, 2), (5, 2), (6, 2), (7, 2), (8, 2),
    (9, 3), (10, 3), (11, 3), (12, 3), (13, 3);

INSERT INTO EXPENSES (CREATION_DATE, AMOUNT, TAG, TITLE, EVENT_ID, ORIGINALPAYER_ID)
VALUES
    ('20230811', 3.50, 'food', 'Eggs', 1, 2),
    ('20230813', 3.0, 'drinks', 'Beers', 1, 2),
    ('20230913', 30.0, 'transportation', 'Uber', 1, 1),
    ('20240913', 100.0, 'food', 'Vegan BBQ ):', 2, 3),
    ('20230913', 200.0, 'transportation', 'Bus', 2, 6),
    ('20230913', 20.0, 'food', 'Chicken', 2, 7),
    ('20230915', 120.0, 'tickets', 'Entry tickets', 3, 12),
    ('20260911', 42.30, 'food', 'Burgers', 3, 13),
    ('20230914', 15.0, 'drinks', 'Orange juice', 3, 11),
    ('20230911', 22.0, 'transportation', 'Train', 3, 13);

INSERT INTO USER_EXPENSES (PAID_AMOUNT, TOTAL_AMOUNT, DEBTOR_ID, EXPENSE_ID)
VALUES
    ( 0.50,  1.75, 1, 1),
    ( 0.00,  1.00, 2, 2),
    ( 0.00, 15.00, 2, 3),
    ( 0.00,  5.00, 4, 4),
    ( 0.00,  5.00, 5, 4),
    ( 0.00,  5.00, 6, 4),
    ( 0.00, 10.00, 7, 4),
    ( 0.00, 10.00, 8, 4),
    ( 0.00,100.00, 5, 5),
    (50.00,100.00, 8, 5),
    (0.00,  10.00, 5, 6),
    (30.00, 30.00, 9, 7),
    ( 0.00, 30.00, 10, 7),
    ( 0.00,  2.00, 13, 8),
    (30.00, 31.50, 9, 8);