// frontend/src/pages/AddTrade.jsx
// TICKET-ADV123 — react-hook-form (uncontrolled inputs) + Yup schema.
// RHF only pays off if the inputs stay uncontrolled — no value/onChange
// wired by hand anywhere below; register() does that work.
import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

const schema = yup.object({
  tradeRef: yup
    .string()
    .matches(/^[A-Z]{3}-\d{8}-\d{4}$/, 'Format: AAA-YYYYMMDD-NNNN')
    .required('Trade ref is required'),
  instrumentId: yup
    .number()
    .typeError('Instrument id must be a number')
    .integer()
    .positive()
    .required('Instrument id is required'),
  counterpartyId: yup
    .number()
    .typeError('Counterparty id must be a number')
    .integer()
    .positive()
    .required('Counterparty id is required'),
  assetClass: yup
    .string()
    .oneOf(['EQUITY', 'FX', 'BOND', 'DERIVATIVE'])
    .required('Asset class is required'),
  side: yup.string().oneOf(['BUY', 'SELL']).required('Side is required'),
  quantity: yup
    .number()
    .typeError('Quantity must be a number')
    .positive('Quantity must be positive')
    .required('Quantity is required'),
  price: yup
    .number()
    .typeError('Price must be a number')
    .positive('Price must be positive')
    .required('Price is required'),
  tradeDate: yup
    .date()
    .typeError('Trade date is required')
    .max(new Date(), 'Trade date cannot be in the future')
    .required('Trade date is required'),
});

function AddTrade() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm({
    resolver: yupResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      tradeRef: '',
      instrumentId: '',
      counterpartyId: '',
      assetClass: 'EQUITY',
      side: 'BUY',
      quantity: '',
      price: '',
      tradeDate: '',
    },
  });

  async function onSubmit(values) {
    await api.createTrade(values);
    reset();
  }

  return (
    <section>
      <h2>Add trade</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="trade-form" noValidate>
        <label>
          Trade ref
          <input {...register('tradeRef')} placeholder="EQU-20260603-0001" />
        </label>
        {errors.tradeRef && <span role="alert">{errors.tradeRef.message}</span>}

        <label>
          Instrument id
          <input type="number" {...register('instrumentId')} />
        </label>
        {errors.instrumentId && <span role="alert">{errors.instrumentId.message}</span>}

        <label>
          Counterparty id
          <input type="number" {...register('counterpartyId')} />
        </label>
        {errors.counterpartyId && <span role="alert">{errors.counterpartyId.message}</span>}

        <label>
          Asset class
          <select {...register('assetClass')}>
            <option value="EQUITY">EQUITY</option>
            <option value="FX">FX</option>
            <option value="BOND">BOND</option>
            <option value="DERIVATIVE">DERIVATIVE</option>
          </select>
        </label>
        {errors.assetClass && <span role="alert">{errors.assetClass.message}</span>}

        <label>
          Side
          <select {...register('side')}>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </label>
        {errors.side && <span role="alert">{errors.side.message}</span>}

        <label>
          Quantity
          <input type="number" step="0.0001" {...register('quantity')} />
        </label>
        {errors.quantity && <span role="alert">{errors.quantity.message}</span>}

        <label>
          Price
          <input type="number" step="0.0001" {...register('price')} />
        </label>
        {errors.price && <span role="alert">{errors.price.message}</span>}

        <label>
          Trade date
          <input type="date" {...register('tradeDate')} />
        </label>
        {errors.tradeDate && <span role="alert">{errors.tradeDate.message}</span>}

        <button disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Submitting…' : 'Submit'}
        </button>
      </form>
    </section>
  );
}

export default withAuth(AddTrade);
