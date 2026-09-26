// Shared between the schedule and the patient page's visit editor.
export const STATUS_LABELS = {
  SCHEDULED: 'Scheduled',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

export const VISIT_STATUSES = Object.keys(STATUS_LABELS);

// Quick actions offered per current status. The API accepts any transition; these are
// just the ones a doctor normally takes next.
export const NEXT_STATUS_ACTIONS = {
  SCHEDULED: [
    { status: 'IN_PROGRESS', label: 'Start' },
    { status: 'CANCELLED', label: 'Cancel', secondary: true },
  ],
  IN_PROGRESS: [{ status: 'COMPLETED', label: 'Complete' }],
};
