import { useEffect, useState } from 'react';
import {
  listMedications, createMedication, restockMedication,
  listPendingPrescriptions, dispensePrescription,
} from '../../services/pharmacyService.js';
import AppShell from '../Layout/AppShell.jsx';
import styles from './PharmacyPage.module.css';

const EMPTY_MED = { name: '', unit: '', stockQuantity: 0, reorderThreshold: 0 };

export default function PharmacyPage() {
  const [medications, setMedications] = useState([]);
  const [queue, setQueue] = useState([]);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_MED);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    listMedications().then(setMedications).catch(() => setError('Failed to load inventory'));
    listPendingPrescriptions().then(setQueue).catch(() => setError('Failed to load prescription queue'));
  }

  useEffect(load, []);

  const lowStockCount = medications.filter((m) => m.active && m.lowStock).length;

  async function handleCreate(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await createMedication({
        name: form.name,
        unit: form.unit,
        stockQuantity: Number(form.stockQuantity),
        reorderThreshold: Number(form.reorderThreshold),
      });
      setForm(EMPTY_MED);
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add medication');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AppShell>
      <div className={styles.content}>
        <div className={styles.header}>
          <h1>Pharmacy</h1>
          {lowStockCount > 0 && <span className={styles.alert}>{lowStockCount} item{lowStockCount > 1 ? 's' : ''} low on stock</span>}
        </div>
        {error && <p className="error">{error}</p>}

        <section className={styles.section}>
          <div className={styles.sectionHead}>
            <h2>Prescriptions to dispense</h2>
          </div>
          <ul className={styles.list}>
            {queue.map((p) => (
              <QueueRow key={p.id} prescription={p} medications={medications} onDispensed={load} />
            ))}
            {queue.length === 0 && <li className={styles.empty}>No prescriptions awaiting dispensing.</li>}
          </ul>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHead}>
            <h2>Inventory</h2>
            {!showForm && <button onClick={() => setShowForm(true)}>+ Add medication</button>}
          </div>

          {showForm && (
            <form onSubmit={handleCreate} className={styles.form}>
              <div className={styles.formRow}>
                <div>
                  <label htmlFor="medName">Name</label>
                  <input id="medName" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                </div>
                <div>
                  <label htmlFor="medUnit">Unit</label>
                  <input id="medUnit" required placeholder="tablets" value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })} />
                </div>
              </div>
              <div className={styles.formRow}>
                <div>
                  <label htmlFor="medStock">Opening stock</label>
                  <input id="medStock" type="number" min="0" required value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} />
                </div>
                <div>
                  <label htmlFor="medThreshold">Reorder threshold</label>
                  <input id="medThreshold" type="number" min="0" required value={form.reorderThreshold} onChange={(e) => setForm({ ...form, reorderThreshold: e.target.value })} />
                </div>
              </div>
              <div className={styles.formActions}>
                <button type="button" className={styles.btnSecondary} onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Add medication'}</button>
              </div>
            </form>
          )}

          <table className={styles.table}>
            <thead>
              <tr><th>Medication</th><th>Stock</th><th>Reorder at</th><th>Status</th><th /></tr>
            </thead>
            <tbody>
              {medications.map((m) => (
                <MedicationRow key={m.id} medication={m} onChanged={load} onError={setError} />
              ))}
              {medications.length === 0 && (
                <tr><td colSpan={5} className={styles.empty}>No medications yet.</td></tr>
              )}
            </tbody>
          </table>
        </section>
      </div>
    </AppShell>
  );
}

function MedicationRow({ medication, onChanged, onError }) {
  const [qty, setQty] = useState('');

  async function handleRestock(e) {
    e.preventDefault();
    try {
      await restockMedication(medication.id, Number(qty));
      setQty('');
      onChanged();
    } catch (err) {
      onError(err.response?.data?.message || 'Failed to restock');
    }
  }

  return (
    <tr>
      <td>{medication.name}</td>
      <td>{medication.stockQuantity} {medication.unit}</td>
      <td>{medication.reorderThreshold}</td>
      <td>
        {!medication.active
          ? <span className={styles.tag}>Inactive</span>
          : medication.lowStock
            ? <span className={styles.tagWarn}>Low stock</span>
            : <span className={styles.tag}>In stock</span>}
      </td>
      <td>
        <form onSubmit={handleRestock} className={styles.inline}>
          <input type="number" min="1" required aria-label={`Restock quantity for ${medication.name}`} placeholder="Qty" value={qty} onChange={(e) => setQty(e.target.value)} />
          <button type="submit">Restock</button>
        </form>
      </td>
    </tr>
  );
}

function QueueRow({ prescription, medications, onDispensed }) {
  const activeMeds = medications.filter((m) => m.active);
  const match = activeMeds.find((m) => m.name.toLowerCase() === prescription.medicationName.toLowerCase());
  const [medicationId, setMedicationId] = useState(match?.id ?? '');
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleDispense(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await dispensePrescription(prescription.id, medicationId, Number(quantity));
      onDispensed();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to dispense');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <li className={styles.item}>
      <div className={styles.itemTop}>
        <span>{prescription.medicationName}</span>
        <span className={styles.tag}>{new Date(prescription.createdAt).toLocaleDateString()}</span>
      </div>
      <div className={styles.itemMeta}>
        {prescription.dosage} · {prescription.frequency}
        {prescription.durationDays ? ` · ${prescription.durationDays} days` : ''}
      </div>
      {prescription.instructions && <div className={styles.itemMeta}>{prescription.instructions}</div>}
      {error && <p className="error">{error}</p>}
      <form onSubmit={handleDispense} className={styles.inline}>
        <select required aria-label="Medication to dispense" value={medicationId} onChange={(e) => setMedicationId(e.target.value)}>
          <option value="">Select medication…</option>
          {activeMeds.map((m) => (
            <option key={m.id} value={m.id}>{m.name} ({m.stockQuantity} {m.unit})</option>
          ))}
        </select>
        <input type="number" min="1" required aria-label="Quantity" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
        <button type="submit" disabled={submitting}>{submitting ? 'Dispensing…' : 'Dispense'}</button>
      </form>
    </li>
  );
}
