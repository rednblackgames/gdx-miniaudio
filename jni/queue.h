#ifndef QUEUE_H
#define QUEUE_H

#define QUEUE_SIZE 100

typedef struct {
    int type;
    ma_sound* sound;
    ma_uint32 level;
    char* message;
    int notificationType;
} Event;

typedef struct {
    Event* buffer[QUEUE_SIZE];
    ma_atomic_uint32 head;   // Points to the next free slot for producers
    ma_atomic_uint32 tail;   // Points to the next item for the consumer to consume
} LockFreeQueue;

// Initialize the lock-free queue
void init_queue(LockFreeQueue *queue) {
    ma_atomic_uint32_set(&queue->head, 0);
    ma_atomic_uint32_set(&queue->tail, 0);
}

// Add an event to the queue (non-blocking, multi-producer safe)
int enqueue(LockFreeQueue *queue, Event* event) {
    ma_uint32 head, tail, next_head;

    do {
        head = ma_atomic_uint32_get(&queue->head);
        tail = ma_atomic_uint32_get(&queue->tail);
        next_head = (head + 1) % QUEUE_SIZE;

        // Check if the queue is full
        if (next_head == tail) {
            return -1; // Queue is full
        }
    } while (!ma_atomic_compare_exchange_weak_32((ma_uint32*) &queue->head, &head, next_head));

    // Store the event
    queue->buffer[head] = event;
    return 0;
}

// Remove an event from the queue (non-blocking, single-consumer)
int dequeue(LockFreeQueue *queue, Event **event) {
    int tail = ma_atomic_uint32_get(&queue->tail);
    int head = ma_atomic_uint32_get(&queue->head);

    // Check if the queue is empty
    if (tail == head) {
        return -1; // Queue is empty
    }

    // Retrieve the event
    *event = queue->buffer[tail];
    ma_atomic_uint32_set(&queue->tail, (tail + 1) % QUEUE_SIZE); // Advance tail
    return 0;
}

#endif