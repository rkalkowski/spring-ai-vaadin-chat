# Order lifecycle

An order in Acme ERP moves through six states:

`DRAFT → CONFIRMED → PICKING → SHIPPED → INVOICED → CLOSED`

Any state except `CLOSED` can transition to `CANCELLED`.

## Rules

- An order can only be confirmed if every line item has a warehouse allocation.
- Confirmed orders lock their prices. Later price-list changes do not affect them.
- Partial shipments create a child order in `PICKING` for the remainder.
- Invoicing requires a valid tax identifier on the customer record.
- `CLOSED` orders are immutable; corrections require a credit note.

## Volume discounts

Discounts are applied per line item, by quantity threshold:

| Quantity | Discount |
|---|---|
| 100–499 | 10% |
| 500–999 | 15% |
| 1000+ | 20% |

Discounts never stack with contract pricing; the lower effective price wins.
