Cryptocurrency Wallet System

- A user holds one wallet per currency balance, tracked in the smallest unit of that currency
- Deposit, withdraw, and peer to peer transfer between wallets
- A balance can never go negative
- A transfer is atomic: both legs land or neither does
- Every movement writes a ledger entry, so a balance is reconstructible from history
- A retried transfer with the same idempotency key does not move money twice
- Concurrent transfers must not deadlock and must not lose or create money

Entities

Currency
Amount
Wallet
LedgerEntry
Transaction

The two things worth saying out loud

1. Money is never a Double. Amounts are integer counts of the smallest unit (satoshi, paisa)
   and every operation checks both sides share a currency.
2. A transfer locks two wallets, so lock them in a fixed global order (by wallet id) or two
   opposite transfers A->B and B->A deadlock. That ordering is the whole trick.

Not modelled
- on chain settlement, gas, exchange rates and conversion, KYC, cold storage, fees
- ETH's 18 decimals overflow a Long, a real system uses BigInteger here
