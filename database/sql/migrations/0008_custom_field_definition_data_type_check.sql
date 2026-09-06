-- 0008_custom_field_definition_data_type_check.sql
-- Constrains custom_field_definition.data_type to the set of types the
-- backend validates and the frontend renders, mirroring the pattern used
-- for workflow_state.category (chk_workflow_state_category).

ALTER TABLE custom_field_definition
    ADD CONSTRAINT chk_custom_field_definition_data_type
        CHECK (data_type IN ('TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'SINGLE_SELECT', 'MULTI_SELECT'));
