Referral & Rewards System

- Every user gets a referral code on signup
- A new user may sign up with someone's code, which creates a PENDING referral
- The referral QUALIFIES when the referee does the qualifying action (first paid order here)
- On qualification, rewards are paid up the referrer chain: tier 1 to the direct referrer,
  tier 2 to whoever referred them, and so on up to the configured depth
- A referral pays out exactly once, no matter how many times qualify is called
- Self referral, double referral, unknown codes, and cycles are all rejected

Entities

Money
User
Referral
Payout
RewardPolicy

Rules the interviewer will poke at
- you can not refer yourself
- a user can be referred only once, ever
- A referring B who referred A is a cycle, walk the chain and refuse it
- qualify is idempotent, a retry must not pay twice
- a chain shorter than the tier depth just pays the tiers that exist

Swappable
- reward policy: flat per tier here, could be percentage of the referee's first order

Not modelled
- fraud scoring, payout to a real wallet or bank, expiry windows, campaign budgets
