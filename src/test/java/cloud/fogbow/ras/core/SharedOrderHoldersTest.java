package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.Order;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.UUID;

@PrepareForTest(DatabaseManager.class)
public class SharedOrderHoldersTest extends BaseUnitTests {

    // test case: As SynchronizedDoublyLinkedList is a sigleton object, when getting the
    // list twice (or more) it must point to the same reference, in other words,
    // they are the same object.
    @Test
    public void testGetSameListReference() throws InternalServerErrorException {
        // set up
        this.testUtils.mockReadOrdersFromDataBase();
        SharedOrderHolders instanceOne = SharedOrderHolders.getInstance();
        SharedOrderHolders instanceTwo = SharedOrderHolders.getInstance();
        SynchronizedDoublyLinkedList<Order> listFromInstanceOne = instanceOne.getOpenOrdersList();
        SynchronizedDoublyLinkedList<Order> listFromInstanceTwo = instanceTwo.getOpenOrdersList();

        // verify
        Assert.assertSame(listFromInstanceOne, listFromInstanceTwo);

        // exercise
        Order orderOne = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        listFromInstanceOne.addItem(orderOne);

        // verify
        Assert.assertSame(listFromInstanceOne.getCurrent(), listFromInstanceTwo.getCurrent());
        Assert.assertSame(orderOne, listFromInstanceOne.getCurrent().getValue());
        Assert.assertSame(orderOne, listFromInstanceTwo.getCurrent().getValue());

        // exercise
        Order orderTwo = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        listFromInstanceTwo.addItem(orderTwo);

        // verify
        Assert.assertSame(
                listFromInstanceOne.getCurrent().getNext(),
                listFromInstanceTwo.getCurrent().getNext());
        Assert.assertSame(orderTwo, listFromInstanceOne.getCurrent().getNext().getValue());
        Assert.assertSame(orderTwo, listFromInstanceTwo.getCurrent().getNext().getValue());
    }

    // test case: When calling the constructor method,
    // it must verify if It It retrieves Orders at the database and populates lists.
    @Test
    public void testConstructorSuccessfully() throws InternalServerErrorException {
        // set up
        int localOpenOrderSize = 2;
        int remoteOpenOrderSize = 3;
        int localPendingOrderSize = 0; // Always 0
        int remotePendingOrderSize = 4;
        int localSelectedOrderSize = 5;
        int remoteSelectedOrderSize = 3;
        int localFulfilledOrderSize = 6;
        int remoteFulfilledOrderSize = 7;
        int localFailedAfterSuccessRequestOrderSize = 8;
        int remoteFailedAfterSuccessRequestOrderSize = 9;
        int localCheckingDeletionOrderSize = 10;
        int remoteCheckingDeletionOrderSize = 0;
        int localSpawningOrderSize = 11;
        int remoteSpawningOrderSize = 12;
        int localFailedOnRequestOrderSize = 14;
        int remoteFailedOnRequestOrderSize = 15;
        int localUnableToCheckRequestOrderSize = 16;
        int remoteUnableToCheckRequestOrderSize = 17;
        int localAssignedForDeletionOrderSize = 18;
        int remoteAssignedForDeletionOrderSize = 19;
        int localPausingOrderSize = 20;
        int remotePausingOrderSize = 21;
        int localResumingOrderSize = 22;
        int remoteResumingOrderSize = 23;
        int localHibernatingOrderSize = 24;
        int remoteHibernatingOrderSize = 25;
        int localHibernatedOrderSize = 26;
        int remoteHibernatedOrderSize = 27;
        int localPausedOrderSize = 28;
        int remotePausedOrderSize = 29;
        int localStoppingOrderSize = 30;
        int remoteStoppingOrderSize = 31;
        int localStoppedOrderSize = 32;
        int remoteStoppedOrderSize = 33;

        int activeOrdersSizeExpected = localOpenOrderSize + remoteOpenOrderSize
                + remotePendingOrderSize
                + localSelectedOrderSize + remoteSelectedOrderSize
                + localFulfilledOrderSize + remoteFulfilledOrderSize
                + localFailedAfterSuccessRequestOrderSize + remoteFailedAfterSuccessRequestOrderSize
                + localCheckingDeletionOrderSize + remoteCheckingDeletionOrderSize
                + localSpawningOrderSize + remoteSpawningOrderSize
                + localFailedOnRequestOrderSize + remoteFailedOnRequestOrderSize
                + localUnableToCheckRequestOrderSize + remoteUnableToCheckRequestOrderSize
                + localAssignedForDeletionOrderSize + remoteAssignedForDeletionOrderSize
                + localPausingOrderSize + remotePausingOrderSize
                + localResumingOrderSize + remoteResumingOrderSize
                + localHibernatingOrderSize + remoteHibernatingOrderSize
                + localHibernatedOrderSize + remoteHibernatedOrderSize
                + localPausedOrderSize + remotePausedOrderSize
                + localStoppingOrderSize + remoteStoppingOrderSize
                + localStoppedOrderSize + remoteStoppedOrderSize;

        int remoteOrdersSizeExpected = localPendingOrderSize + remotePendingOrderSize
                + remoteSelectedOrderSize
                + remoteFulfilledOrderSize
                + remoteFailedAfterSuccessRequestOrderSize
                + remoteCheckingDeletionOrderSize
                + remoteSpawningOrderSize
                + remoteFailedOnRequestOrderSize
                + remoteUnableToCheckRequestOrderSize
                + remoteAssignedForDeletionOrderSize
                + remotePausingOrderSize
                + remoteResumingOrderSize
                + remoteHibernatingOrderSize
                + remoteHibernatedOrderSize
                + remotePausedOrderSize
                + remoteStoppingOrderSize
                + remoteStoppedOrderSize;

        int openOrderListSizeExpected = localOpenOrderSize + remoteOpenOrderSize;
        int selectedOrderListSizeExpected = localSelectedOrderSize;
        int fulfilledOrderListSizeExpecte = localFulfilledOrderSize;
        int failedAfterSuccessRequestOrderListSizeExpected = localFailedAfterSuccessRequestOrderSize;
        int checkingDeletionOrderListSizeExpected = localCheckingDeletionOrderSize;
        int spawningOrderListSizeExpected = localSpawningOrderSize;
        int failedOnRequestOrderListSizeExpected = localFailedOnRequestOrderSize;
        int unableToCheckRequestOrderListSizeExpected = localUnableToCheckRequestOrderSize;
        int assignedForDeletionOrderListSizeExpected = localAssignedForDeletionOrderSize;
        int pausingOrderListSizeExpected = localPausingOrderSize;
        int resumingOrderListSizeExpected = localResumingOrderSize;
        int hibernatingOrderListSizeExpected = localHibernatingOrderSize;
        int hibernatedOrderListSizeExpected = localHibernatedOrderSize;
        int pausedOrderListSizeExpected = localPausedOrderSize;
        int stoppingOrderListSizeExpected = localStoppingOrderSize;
        int stoppedOrderListSizeExpected = localStoppedOrderSize;

        SynchronizedDoublyLinkedList<Order> openList = createOrderList(localOpenOrderSize, remoteOpenOrderSize);
        SynchronizedDoublyLinkedList<Order> selectedList = createOrderList(localSelectedOrderSize, remoteSelectedOrderSize);
        SynchronizedDoublyLinkedList<Order> fulfilledList = createOrderList(localFulfilledOrderSize, remoteFulfilledOrderSize);
        SynchronizedDoublyLinkedList<Order> failedAfterSuccessRequestList = createOrderList(
                localFailedAfterSuccessRequestOrderSize, remoteFailedAfterSuccessRequestOrderSize);
        SynchronizedDoublyLinkedList<Order> checkingDeletionList = createOrderList(
                localCheckingDeletionOrderSize, remoteCheckingDeletionOrderSize);
        SynchronizedDoublyLinkedList<Order> pendingList = createOrderList(localPendingOrderSize, remotePendingOrderSize);
        SynchronizedDoublyLinkedList<Order> spawningList = createOrderList(localSpawningOrderSize, remoteSpawningOrderSize);
        SynchronizedDoublyLinkedList<Order> failedOnRequestList = createOrderList(
                localFailedOnRequestOrderSize, remoteFailedOnRequestOrderSize);
        SynchronizedDoublyLinkedList<Order> unableToCheckRequestList = createOrderList(
                localUnableToCheckRequestOrderSize, remoteUnableToCheckRequestOrderSize);
        SynchronizedDoublyLinkedList<Order> assignedForDeletionRequestList = createOrderList(
                localAssignedForDeletionOrderSize, remoteAssignedForDeletionOrderSize);
        SynchronizedDoublyLinkedList<Order> pausingRequestList = createOrderList(
                localPausingOrderSize, remotePausingOrderSize);
        SynchronizedDoublyLinkedList<Order> resumingRequestList = createOrderList(
                localResumingOrderSize, remoteResumingOrderSize);
        SynchronizedDoublyLinkedList<Order> hibernatingRequestList = createOrderList(
                localHibernatingOrderSize, remoteHibernatingOrderSize);
        SynchronizedDoublyLinkedList<Order> hibernatedRequestList = createOrderList(
                localHibernatedOrderSize, remoteHibernatedOrderSize);
        SynchronizedDoublyLinkedList<Order> pausedRequestList = createOrderList(
                localPausedOrderSize, remotePausedOrderSize);
        SynchronizedDoublyLinkedList<Order> stoppingRequestList = createOrderList(
                localStoppingOrderSize, remoteStoppingOrderSize);
        SynchronizedDoublyLinkedList<Order> stoppedRequestList = createOrderList(
                localStoppedOrderSize, remoteStoppedOrderSize);

        this.testUtils.mockReadOrdersFromDataBase(openList, selectedList, fulfilledList, failedAfterSuccessRequestList,
                checkingDeletionList, pendingList, spawningList, failedOnRequestList, unableToCheckRequestList,
                assignedForDeletionRequestList, pausingRequestList, resumingRequestList, hibernatingRequestList,
                hibernatedRequestList, pausedRequestList, stoppingRequestList, stoppedRequestList);

        // exercise
        SharedOrderHolders sharedOrderHolders = new SharedOrderHolders();

        // verify
        int activeOrderSize = sharedOrderHolders.getActiveOrdersMap().size();
        Assert.assertEquals(activeOrdersSizeExpected, activeOrderSize);

        checkList(remoteOrdersSizeExpected, sharedOrderHolders.getRemoteProviderOrdersList());
        checkList(openOrderListSizeExpected, sharedOrderHolders.getOpenOrdersList());
        checkList(selectedOrderListSizeExpected, sharedOrderHolders.getSelectedOrdersList());
        checkList(fulfilledOrderListSizeExpecte, sharedOrderHolders.getFulfilledOrdersList());
        checkList(failedAfterSuccessRequestOrderListSizeExpected, sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList());
        checkList(checkingDeletionOrderListSizeExpected, sharedOrderHolders.getCheckingDeletionOrdersList());
        checkList(spawningOrderListSizeExpected, sharedOrderHolders.getSpawningOrdersList());
        checkList(failedOnRequestOrderListSizeExpected, sharedOrderHolders.getFailedOnRequestOrdersList());
        checkList(unableToCheckRequestOrderListSizeExpected, sharedOrderHolders.getUnableToCheckStatusOrdersList());
        checkList(assignedForDeletionOrderListSizeExpected, sharedOrderHolders.getAssignedForDeletionOrdersList());
        checkList(pausingOrderListSizeExpected, sharedOrderHolders.getPausingOrdersList());
        checkList(resumingOrderListSizeExpected, sharedOrderHolders.getResumingOrdersList());
        checkList(hibernatingOrderListSizeExpected, sharedOrderHolders.getHibernatingOrdersList());
        checkList(hibernatedOrderListSizeExpected, sharedOrderHolders.getHibernatedOrdersList());
        checkList(pausedOrderListSizeExpected, sharedOrderHolders.getPausedOrdersList());
        checkList(stoppingOrderListSizeExpected, sharedOrderHolders.getStoppingOrdersList());
        checkList(stoppedOrderListSizeExpected, sharedOrderHolders.getStoppedOrdersList());
    }

    private void checkList(int sizeExpected, SynchronizedDoublyLinkedList<Order> list) {
        int listSize = 0;
        while (list.getNext() != null) {
            listSize++;
        }
        Assert.assertEquals(sizeExpected, listSize);
    }

    private SynchronizedDoublyLinkedList<Order> createOrderList(int sizeLocal, int sizeRemote) throws InternalServerErrorException {
        SynchronizedDoublyLinkedList<Order> list = new SynchronizedDoublyLinkedList<>();
        for (int i = 0; i < sizeLocal; i++) {
            Order order = Mockito.mock(Order.class);
            Mockito.when(order.isProviderRemote(Mockito.any())).thenReturn(false);
            Mockito.when(order.getId()).thenReturn(UUID.randomUUID().toString());
            list.addItem(order);
        }

        for (int i = 0; i < sizeRemote; i++) {
            Order order = Mockito.mock(Order.class);
            Mockito.when(order.isProviderRemote(Mockito.any())).thenReturn(true);
            Mockito.when(order.getId()).thenReturn(UUID.randomUUID().toString());
            list.addItem(order);
        }
        return list;
    }

}
