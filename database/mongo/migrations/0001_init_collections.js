// 0001_init_collections.js
// Creates the work_item_content collection (see docs/backlog-data-model.md)
// that holds the flexible/free-form content for every work_item row in
// PostgreSQL: description, custom field values, comments, attachments.
//
// Runs against whichever database the connection URI points at (see
// database/migrate.js), so no getSiblingDB() call is needed here.

db.createCollection('work_item_content', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['work_item_id'],
      properties: {
        work_item_id: {
          bsonType: 'string',
          description: 'UUID of the related work_item row in Postgres'
        },
        description: { bsonType: ['string', 'null'] },
        custom_fields: { bsonType: 'object' },
        comments: { bsonType: 'array' },
        attachments: { bsonType: 'array' }
      }
    }
  },
  validationLevel: 'moderate'
});

db.work_item_content.createIndex(
  { work_item_id: 1 },
  { unique: true, name: 'uniq_work_item_id' }
);

print('work_item_content collection and indexes created.');
