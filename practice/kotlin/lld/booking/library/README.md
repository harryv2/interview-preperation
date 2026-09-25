Library Management System

- Member searches the catalogue by title, author or ISBN
- A book has many physical copies, each with its own barcode
- Member borrows a free copy, up to a loan limit, for a fixed period
- If every copy is out the member can place a hold, served first in first out
- Returning a copy hands it to the first holder, otherwise back to the shelf
- A late return is fined per day
- Member with unpaid fines over a limit can not borrow

Entities

Money
Book
BookCopy
Member
Loan
Hold
Catalogue

Copy status

AVAILABLE -> LOANED -> AVAILABLE
AVAILABLE -> RESERVED (held for the next member in the queue)

Swappable
- fine policy
- loan limit and loan period per membership tier

Not modelled
- payments, acquisitions, inter library transfer, e-books
