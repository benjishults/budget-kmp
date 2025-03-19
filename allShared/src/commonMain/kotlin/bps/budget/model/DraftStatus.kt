package bps.budget.model

/**
 * Draft and charge transactions have some transaction items that are outstanding and some that are cleared.
 * Real accounts have "clearing" transaction items that clear a check or pay a charge bill.
 */
enum class DraftStatus {
    none,

    /**
     * Means that the item is a cleared draft or charge expense on a category account from a draft or charge account
     */
    cleared,

    /**
     * Means that it is part of a clearing transaction transferring from a real account to a draft or charge account
     */
    clearing,

    /**
     * Means that the item is a draft or charge expense on a category account from a draft or charge account
     * waiting for a clearing event on a real account
     */
    outstanding
}
