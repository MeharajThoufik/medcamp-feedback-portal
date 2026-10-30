-- App-wide settings that admins change from the screen, such as the camp Wi-Fi printed on posters.
CREATE TABLE settings (
    setting_key   VARCHAR(100)             PRIMARY KEY,
    setting_value VARCHAR(500)             NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
