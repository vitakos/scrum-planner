-- 0009_custom_field_definition_rich_text.sql
-- Adds RICH_TEXT as a valid custom field data type, so a custom field can
-- use the same rich Markdown editor as the built-in Description field
-- (see PredefinedFieldCatalog's "description" entry, which already reports
-- RICH_TEXT). Values are still stored as free-form Markdown text in Mongo,
-- same as TEXT — this only changes which editor the frontend renders.

ALTER TABLE custom_field_definition
    DROP CONSTRAINT chk_custom_field_definition_data_type;

ALTER TABLE custom_field_definition
    ADD CONSTRAINT chk_custom_field_definition_data_type
        CHECK (data_type IN ('TEXT', 'RICH_TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'SINGLE_SELECT', 'MULTI_SELECT'));
