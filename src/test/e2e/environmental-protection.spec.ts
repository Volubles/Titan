import { test, waitForStable, waitUntil } from '@drownek/paperwright';
import { Vec3 } from 'vec3';

const explosionTarget = new Vec3(45, 65, 41);
const pistonSource = new Vec3(51, 65, 41);
const pistonDestination = new Vec3(52, 65, 41);

test('protected worlds deny explosion damage and piston movement', async ({ player, server }) => {
  await player.makeOp();
  await player.teleport(49, 67, 43);
  await waitUntil(
    () => player.bot.blockAt(explosionTarget) !== null
      && player.bot.blockAt(pistonDestination) !== null,
    { message: 'Expected environmental protection chunks to load' },
  );

  server.execute('setblock 45 65 41 stone');
  server.execute('setblock 50 65 41 piston[facing=east]');
  server.execute('setblock 51 65 41 stone');
  server.execute('setblock 52 65 41 air');
  await player.deOp();

  await waitUntil(
    () => player.bot.blockAt(explosionTarget)?.name === 'stone'
      && player.bot.blockAt(pistonSource)?.name === 'stone'
      && player.bot.blockAt(pistonDestination)?.name === 'air',
    { message: 'Expected environmental protection fixtures to reach the bot' },
  );

  server.execute('summon tnt 45.5 65 41.5 {fuse:1}');
  await waitForStable(
    () => player.bot.blockAt(explosionTarget)?.name === 'stone',
    {
      duration: 1500,
      timeout: 5000,
      message: 'Expected explosion block damage to be denied',
    },
  );

  server.execute('setblock 49 65 41 redstone_block');
  await waitForStable(
    () => player.bot.blockAt(pistonSource)?.name === 'stone'
      && player.bot.blockAt(pistonDestination)?.name === 'air',
    {
      duration: 1000,
      timeout: 5000,
      message: 'Expected piston movement to be denied',
    },
  );
});
