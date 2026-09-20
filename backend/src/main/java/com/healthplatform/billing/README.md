# billing module — extension point

Not yet implemented. Follows the same layering as `auth` and `patient`:
`controller/`, `service/`, `repository/`, `model/`, `dto/`.

Scope for this module is defined in `docs/SRS.md`. See `docs/ARCHITECTURE.md`
for how it should communicate with other modules (own tables only, cross-module
communication via Kafka events, never direct repository access into another
module's schema).
