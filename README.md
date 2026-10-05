# ph-ee-connector-mt

Payment Hub connector for **Monétique Tunisie** (Société Monétique Tunisie — national card switch).

## Specification alignment

| Capability | Industry / local reference | This connector |
|------------|---------------------------|----------------|
| Authorization | ISO 8583 auth (0100/0110), field 39 response | `MtAuthorizationRequest`, `POST …/authorizations` |
| Capture | Financial presentment / completion | `MtCaptureRequest`, `POST …/captures` |
| Refund | Credit to cardholder | `MtRefundRequest`, `POST …/refunds` |
| Void / reversal | ISO 8583 reversal (0400/0420) | `MtVoidRequest`, `POST …/voids` |
| Status | Host inquiry | `GET …/transactions/{id}` |

TIDs/MIDs and WORKPOS reconciliation are part of the SMT acquirer model; this connector exposes the **authorization lifecycle** used by Payment Hub BPMN.

### Official / public references

- [Monétique Tunisie](https://monetiquetunisie.com/) — national switch operator.
- [BCT — payment systems overview](https://www.bct.gov.tn/bct/siteprod/page.jsp?id=96) — SMT net settlement with SIBTEL/SGMT.
- [BCT circular 2024-5 — payment system managers (PDF annex)](https://www.jurisitetunisie.com/download_bct.php?f=BCTC2024_005-A20240213.pdf) — technical/security requirements for participants (no public ISO 8583 field manual).
- [SMT / TID–MID hierarchy (industry summary)](https://tpe.tn/reseau-smt-tunpay) — terminal and merchant identifiers.

**Scope note:** Banks receive proprietary host specs from Monétique Tunisie after affiliation. This repo implements a **REST façade** with ISO 8583-style response codes (`00`, `05`, …) for Hub integration; binary ISO 8583 framing belongs in the production gateway behind `mt.gateway.base-url`.

## Configuration

| Property | Description |
|----------|-------------|
| `mt.gateway.base-url` | Monétique Tunisie gateway base URL |
| `mt.gateway.merchant-id` | Default merchant id |
| `mt.callback-base-url` | Public URL for callbacks |

## REST (connector)

- `POST /api/v1/mt/authorizations`
- `POST /api/v1/mt/captures`
- `POST /api/v1/mt/refunds`
- `POST /api/v1/mt/voids`
- `GET /api/v1/mt/transactions/{id}`
- `POST /api/v1/mt/callbacks`

## Zeebe workers

`mtAuthorizePayment`, `mtCapturePayment`, `mtRefundPayment`, `mtVoidPayment`, `mtGetStatus`, `mtSettleInbound`

## Gateway API (mocked by simulateur-app)

- `POST /mt/v1/authorizations`
- `POST /mt/v1/captures`
- `POST /mt/v1/refunds`
- `POST /mt/v1/voids`
- `GET /mt/v1/transactions/{id}`
