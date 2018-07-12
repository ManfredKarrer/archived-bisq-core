/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao.state.ext;

import lombok.Getter;

/**
 * All parameters in the Bisq DAO which can be changed by voting.
 * We will add more on demand.
 * We need to support updates with new types in future. We use in the persisted data only the enum name, thus the names
 * must not change once the dao has started. Default values must not be changed as well.
 * For parameters which are used by Bisq users (trade fee,...) we have more strict requirements for backward compatibility.
 * Parameters which are only used in proposals and voting are less strict limited as we can require that those users are
 * using the latest software version.
 * The UNDEFINED entry is used as fallback for error cases and will get ignored.
 */
public enum Param {
    UNDEFINED(-1),
    // Trade fees in BSQ
    MIN_MAKER_FEE_IN_BSQ(5),
    MIN_TAKER_FEE_IN_BSQ(5),
    // TODO That fee should be changed to % fee to be less dependent on btc price
    DEFAULT_MAKER_FEE_IN_BSQ(200), // about 2 USD at 1 BSQ = 1 USD for a 1 BTC trade
    DEFAULT_TAKER_FEE_IN_BSQ(200),

    // Trade fees in BTC
    MIN_MAKER_FEE_IN_BTC(5_000), // 0.5 USD at BTC price 10000 USD
    MIN_TAKER_FEE_IN_BTC(5_000),
    DEFAULT_MAKER_FEE_IN_BTC(200_000), // 20 USD at BTC price 10000 USD for a 1 BTC trade
    DEFAULT_TAKER_FEE_IN_BTC(200_000),

    // Fees proposal/voting. Atm we don't use diff. fees for diff. proposal types
    PROPOSAL_FEE(100),          // 5 BSQ    TODO change low dev
    BLIND_VOTE_FEE(200),        // 10 BSQ   TODO change low dev

    // Quorum for voting in BSQ stake
    QUORUM_PROPOSAL(100),           // 10 000 BSQ  TODO change low dev value
    QUORUM_COMP_REQUEST(100),       // 10 000 BSQ  TODO change low dev value
    QUORUM_CHANGE_PARAM(100),       // 100 000 BSQ TODO change low dev value
    QUORUM_REMOVE_ASSET(100),       // 10 000 BSQ  TODO change low dev value

    // Threshold for voting in % with precision of 2 (e.g. 5000 -> 50.00%)
    THRESHOLD_PROPOSAL(5_000),          // 50%
    THRESHOLD_COMP_REQUEST(5_000),      // 50%
    THRESHOLD_CHANGE_PARAM(7_500),      // 75% -> that might change the THRESHOLD_CHANGE_PARAM and QUORUM_CHANGE_PARAM!
    THRESHOLD_REMOVE_ASSET(5_000),      // 50%

    // Period phase (16 blocks atm)
    PHASE_UNDEFINED(0),
    PHASE_PROPOSAL(3),      // 24 days
    PHASE_BREAK1(2),        // 10 blocks
    PHASE_BLIND_VOTE(3),    // 4 days
    PHASE_BREAK2(2),        // 10 blocks
    PHASE_VOTE_REVEAL(3),   // 2 days
    PHASE_BREAK3(1),        // 10 blocks
    PHASE_RESULT(1),        // 1 block
    PHASE_BREAK4(1),        // 10 blocks

    /*PHASE_UNDEFINED(0),
    PHASE_PROPOSAL(3456),      // 24 days
    PHASE_BREAK1(10),        // 10 blocks
    PHASE_BLIND_VOTE(576),    // 4 days
    PHASE_BREAK2(1),        // 10 blocks
    PHASE_VOTE_REVEAL(432),   // 2 days
    PHASE_BREAK3(10),        // 10 blocks
    PHASE_RESULT(1),        // 1 block
    PHASE_BREAK4(10);        // 10 blocks*/

    //TODO SQ: do we need to set a min value? is max value is max int value it is also not needed to set it as param
    LOCK_TIME_MIN(0),
    LOCK_TIME_MAX(65535);

    @Getter
    private long defaultValue;

    /**
     * @param defaultValue for param. If not set it is -1.
     */
    Param(int defaultValue) {
        this.defaultValue = defaultValue;
    }
}
