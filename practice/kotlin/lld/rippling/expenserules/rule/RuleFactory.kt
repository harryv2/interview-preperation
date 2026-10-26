package lld.rippling.expenserules.rule

import lld.rippling.expenserules.entity.Severity
import java.math.BigDecimal

class RuleSet(
    val expenseRules: List<ExpenseRule>,
    val groupRules: List<GroupRule>,
)

object RuleFactory {

    fun fromConfig(configs: List<Map<String, Any>>): RuleSet {
        val expenseRules = ArrayList<ExpenseRule>()
        val groupRules = ArrayList<GroupRule>()
        for (config in configs) {
            if (config.containsKey("groupBy")) {
                groupRules += groupRule(config)
            } else {
                expenseRules += expenseRule(config)
            }
        }
        return RuleSet(expenseRules, groupRules)
    }

    private fun expenseRule(config: Map<String, Any>): ExpenseRule {
        val severity = severity(config)
        return when (val type = string(config, "type")) {
            "max_amount" -> MaxAmountRule(money(config, "limit"), severity)
            "vendor_type_limit" -> VendorTypeLimitRule(string(config, "vendorType"), money(config, "limit"), severity)
            "banned_expense_type" -> BannedExpenseTypeRule(string(config, "expenseType"), severity)
            "banned_vendor" -> BannedVendorRule(string(config, "vendorName"), severity)
            "all_of" -> AllOfRule(string(config, "name"), children(config), severity)
            "any_of" -> AnyOfRule(string(config, "name"), children(config), severity)
            else -> throw IllegalArgumentException("Unknown rule type $type")
        }
    }

    private fun groupRule(config: Map<String, Any>): GroupRule {
        val severity = severity(config)
        val groupBy = GroupBy.valueOf(string(config, "groupBy").uppercase())
        return when (val type = string(config, "type")) {
            "total_limit" -> GroupTotalLimitRule(groupBy, money(config, "limit"), severity)
            "expense_type_limit" -> GroupExpenseTypeLimitRule(groupBy, string(config, "expenseType"), money(config, "limit"), severity)
            else -> throw IllegalArgumentException("Unknown group rule type $type")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun children(config: Map<String, Any>): List<ExpenseRule> {
        val rules = config["rules"] as? List<Map<String, Any>> ?: throw IllegalArgumentException("Missing rules in $config")
        return rules.map { expenseRule(it) }
    }

    private fun severity(config: Map<String, Any>): Severity {
        val value = config["severity"] ?: return Severity.BLOCK
        return Severity.valueOf(value.toString().uppercase())
    }

    private fun string(config: Map<String, Any>, key: String): String {
        return requireNotNull(config[key]) { "Missing $key in $config" }.toString()
    }

    private fun money(config: Map<String, Any>, key: String): BigDecimal {
        return BigDecimal(string(config, key))
    }
}
