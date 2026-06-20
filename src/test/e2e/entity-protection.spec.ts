import { test, waitForStable, waitUntil } from '@drownek/paperwright';

test('protected worlds deny entity containers and vehicle entry', async ({ player, server }) => {
  await player.makeOp();
  await player.teleport(66, 65, 63);

  server.execute('summon chest_minecart 65.5 65 61.5');
  server.execute('summon oak_boat 68.5 65 61.5');
  await player.deOp();

  await waitUntil(
    () => player.bot.nearestEntity(entity => entity.name === 'chest_minecart') !== null
      && player.bot.nearestEntity(entity => entity.name === 'oak_boat') !== null,
    { message: 'Expected entity protection fixtures to reach the bot' },
  );

  const chestMinecart = player.bot.nearestEntity(entity => entity.name === 'chest_minecart');
  if (!chestMinecart) throw new Error('Chest minecart fixture was not loaded');
  await player.bot.activateEntity(chestMinecart);
  await waitForStable(
    () => player.bot.currentWindow === null,
    { duration: 500, message: 'Expected chest minecart inventory to remain closed' },
  );

  const boat = player.bot.nearestEntity(entity => entity.name === 'oak_boat');
  if (!boat) throw new Error('Boat fixture was not loaded');
  await player.bot.activateEntity(boat);
  await waitForStable(
    () => player.bot.entity.vehicle == null,
    { duration: 500, message: 'Expected vehicle entry to be denied' },
  );
});
