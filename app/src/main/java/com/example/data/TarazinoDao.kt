package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TarazinoDao {

    // Counterparties
    @Query("SELECT * FROM counterparties ORDER BY id DESC")
    fun observeAllCounterparties(): Flow<List<CounterpartyEntity>>

    @Query("SELECT * FROM counterparties ORDER BY id DESC")
    suspend fun getAllCounterpartiesOnce(): List<CounterpartyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterparty(entity: CounterpartyEntity): Long

    @Update
    suspend fun updateCounterparty(entity: CounterpartyEntity)

    @Query("DELETE FROM counterparties WHERE id = :id")
    suspend fun deleteCounterpartyById(id: Long)

    // Obligations
    @Query("SELECT * FROM obligations ORDER BY firstDueDateJalali ASC, id DESC")
    fun observeAllObligations(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations ORDER BY firstDueDateJalali ASC, id DESC")
    suspend fun getAllObligationsOnce(): List<ObligationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligation(obligation: ObligationEntity): Long

    @Update
    suspend fun updateObligation(obligation: ObligationEntity)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteObligationById(id: Long)

    @Query("DELETE FROM obligations WHERE counterpartyId = :counterpartyId")
    suspend fun deleteObligationsByCounterparty(counterpartyId: Long)

    // Installments
    @Query("SELECT * FROM installments ORDER BY dueDateJalali ASC, installmentNumber ASC")
    fun observeAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments ORDER BY dueDateJalali ASC, installmentNumber ASC")
    suspend fun getAllInstallmentsOnce(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE obligationId = :obligationId ORDER BY installmentNumber ASC")
    suspend fun getInstallmentsForObligation(obligationId: Long): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteInstallmentById(id: Long)

    @Query("DELETE FROM installments WHERE obligationId = :obligationId")
    suspend fun deleteInstallmentsByObligation(obligationId: Long)

    // Ledger Sub-Records
    @Query("SELECT * FROM ledger_sub_records ORDER BY dateJalali DESC, id DESC")
    fun observeAllSubRecords(): Flow<List<LedgerSubRecordEntity>>

    @Query("SELECT * FROM ledger_sub_records ORDER BY dateJalali DESC, id DESC")
    suspend fun getAllSubRecordsOnce(): List<LedgerSubRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubRecord(subRecord: LedgerSubRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubRecords(subRecords: List<LedgerSubRecordEntity>)

    @Query("DELETE FROM ledger_sub_records WHERE id = :id")
    suspend fun deleteSubRecordById(id: Long)

    @Query("DELETE FROM ledger_sub_records WHERE obligationId = :obligationId")
    suspend fun deleteSubRecordsByObligation(obligationId: Long)

    // Full Reset
    @Query("DELETE FROM installments")
    suspend fun clearAllInstallments()

    @Query("DELETE FROM ledger_sub_records")
    suspend fun clearAllSubRecords()

    @Query("DELETE FROM obligations")
    suspend fun clearAllObligations()

    @Query("DELETE FROM counterparties")
    suspend fun clearAllCounterparties()
}
