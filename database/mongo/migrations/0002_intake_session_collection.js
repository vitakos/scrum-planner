// 0002_intake_session_collection.js (AISC-97)
// Creates the intake_session collection for storing intake request chat sessions per project.
// Holds messages, attachments, and session metadata.
//
// Runs against whichever database the connection URI points at (see
// database/migrate.js), so no getSiblingDB() call is needed here.

db.createCollection('intake_session', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['project_id', 'messages'],
      properties: {
        _id: {
          bsonType: 'objectId',
          description: 'MongoDB document ID'
        },
        project_id: {
          bsonType: 'string',
          description: 'UUID of the project this session belongs to'
        },
        messages: {
          bsonType: 'array',
          description: 'Array of intake messages',
          items: {
            bsonType: 'object',
            required: ['id', 'sender', 'text', 'timestamp'],
            properties: {
              id: { bsonType: 'string' },
              sender: { bsonType: 'string' },
              text: { bsonType: 'string' },
              timestamp: { bsonType: 'date' },
              attachments: {
                bsonType: 'array',
                items: {
                  bsonType: 'object',
                  properties: {
                    id: { bsonType: 'string' },
                    name: { bsonType: 'string' },
                    size: { bsonType: 'int' },
                    path: { bsonType: ['string', 'null'] }
                  }
                }
              }
            }
          }
        },
        created_at: {
          bsonType: 'date',
          description: 'When this session was created'
        },
        updated_at: {
          bsonType: 'date',
          description: 'When this session was last updated'
        }
      }
    }
  },
  validationLevel: 'moderate'
});

db.intake_session.createIndex(
  { project_id: 1 },
  { unique: true, name: 'uniq_project_id' }
);

db.intake_session.createIndex(
  { updated_at: 1 },
  { name: 'idx_updated_at' }
);

print('intake_session collection and indexes created.');
